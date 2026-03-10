import asyncio
import aiohttp
import time
import requests
import uuid
import logging
import sys

# Configure professional logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("SimulationEngine")

ORCHESTRATOR_URL = "http://localhost:8080/api/trips/book"
AUDIT_URL = "http://localhost:8080/api/trips/audit"
TOTAL_REQUESTS = 1000

async def send_booking(session, req_id):
    payload = {
        "customerId": f"user-{req_id}-{str(uuid.uuid4())[:8]}",
        "flightDetails": "Flight FOO-BAR 100",
        "hotelDetails": "Hotel Hilton NY",
        "carDetails": "Tesla Model 3"
    }
    try:
        async with session.post(ORCHESTRATOR_URL, json=payload, timeout=aiohttp.ClientTimeout(total=5)) as response:
            if response.status not in (200, 202):
                logger.warning(f"Request {req_id} returned non-success status: {response.status}")
            await response.text()
    except aiohttp.ClientConnectorError:
        logger.error(f"Request {req_id} failed: Could not connect to Orchestrator at {ORCHESTRATOR_URL}")
    except asyncio.TimeoutError:
        logger.error(f"Request {req_id} failed: Timeout while waiting for Orchestrator.")
    except Exception as e:
        logger.error(f"Request {req_id} encountered an unexpected error: {str(e)}")

async def main():
    logger.info(f"Starting resilient simulation of {TOTAL_REQUESTS} saga requests...")
    start_time = time.time()
    
    try:
        async with aiohttp.ClientSession() as session:
            tasks = [send_booking(session, i) for i in range(TOTAL_REQUESTS)]
            await asyncio.gather(*tasks)
            
        duration = time.time() - start_time
        logger.info(f"Finished sending requests in {duration:.2f} seconds.")
        
        logger.info("Waiting 15 seconds for Kafka events to settle... (Eventual Consistency window)")
        time.sleep(15)
        
        logger.info("Fetching Audit Summary from Orchestrator...")
        try:
            response = requests.get(AUDIT_URL, timeout=10)
            response.raise_for_status()
            data = response.json()
            
            logger.info("\n--- MATHEMATICAL PROOF OF CONSISTENCY ---")
            total = data.get("TOTAL_REQUESTS", 0)
            completed = data.get("COMPLETED", 0)
            compensated = data.get("COMPENSATED", 0)
            
            logger.info(f"Total Requests Received: {total}")
            logger.info(f"Total Completely Booked: {completed} (~80%)")
            logger.info(f"Total Successfully Compensated (Rolled Back): {compensated} (~20%)")
            
            # Any stuck in intermediate states?
            in_progress = total - (completed + compensated)
            logger.info(f"Total Stuck/Incomplete Sagas: {in_progress}")
            
            if in_progress == 0:
                logger.info("SUCCESS: 100% of Sagas reached a final consistent state!")
            else:
                logger.warning("WARNING: Some Sagas are stuck. Check Zipkin tracing!")
                for state, count in data.items():
                    if state not in ["TOTAL_REQUESTS", "COMPLETED", "COMPENSATED"]:
                        logger.warning(f" - State: {state}, Count: {count}")
        except requests.exceptions.ConnectionError:
            logger.error(f"Audit failed: Could not connect to Orchestrator at {AUDIT_URL}")
        except requests.exceptions.Timeout:
            logger.error("Audit failed: Timeout while communicating with Orchestrator")
        except Exception as e:
            logger.error(f"Audit failed due to unexpected error: {str(e)}")
            
    except KeyboardInterrupt:
        logger.info("Simulation interrupted by user. Exiting gracefully.")
        sys.exit(0)

if __name__ == "__main__":
    # Note: Requires `pip install aiohttp requests`
    try:
        if sys.platform == 'win32':
             asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
        asyncio.run(main())
    except Exception as e:
        logger.critical(f"Fatal error during simulation execution: {str(e)}")
        sys.exit(1)

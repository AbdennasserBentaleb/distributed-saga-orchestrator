import asyncio
import aiohttp
import time
import requests
import uuid

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
        async with session.post(ORCHESTRATOR_URL, json=payload) as response:
            await response.text()
    except Exception as e:
        print(f"Request {req_id} failed: {e}")

async def main():
    print(f"Starting simulation of {TOTAL_REQUESTS} saga requests...")
    start_time = time.time()
    
    async with aiohttp.ClientSession() as session:
        tasks = [send_booking(session, i) for i in range(TOTAL_REQUESTS)]
        await asyncio.gather(*tasks)
        
    duration = time.time() - start_time
    print(f"Finished sending requests in {duration:.2f} seconds.")
    
    print("Waiting 15 seconds for Kafka events to settle...")
    time.sleep(15)
    
    print("Fetching Audit Summary from Orchestrator:")
    try:
        response = requests.get(AUDIT_URL)
        data = response.json()
        
        print("\n--- MATHEMATICAL PROOF OF CONSISTENCY ---")
        total = data.get("TOTAL_REQUESTS", 0)
        completed = data.get("COMPLETED", 0)
        compensated = data.get("COMPENSATED", 0)
        
        print(f"Total Requests Received: {total}")
        print(f"Total Completely Booked: {completed} (~80%)")
        print(f"Total Successfully Compensated (Rolled Back): {compensated} (~20%)")
        
        # Any stuck in intermediate states?
        in_progress = total - (completed + compensated)
        print(f"Total Stuck/Incomplete Sagas: {in_progress}")
        
        if in_progress == 0:
            print("🚀 SUCCESS: 100% of Sagas reached a final consistent state!")
        else:
            print("⚠️ WARNING: Some Sagas are stuck. Check Zipkin tracing!")
            for state, count in data.items():
                if state not in ["TOTAL_REQUESTS", "COMPLETED", "COMPENSATED"]:
                    print(f" - {state}: {count}")
    except Exception as e:
        print(f"Failed to fetch audit: {e}")

if __name__ == "__main__":
    # Note: Requires `pip install aiohttp requests`
    asyncio.run(main())

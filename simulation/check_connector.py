import urllib.request
import json

req = urllib.request.Request("http://localhost:8084/connectors/saga-outbox-connector")
try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode('utf-8'))
except Exception as e:
    print(e)

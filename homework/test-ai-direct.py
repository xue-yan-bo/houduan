import urllib.request
import json

url = "http://127.0.0.1:18080/api/wrong-title-book/aiChart?wrongTitleId=1"
try:
    print("Testing aiChart for ID 1")
    req = urllib.request.Request(url)
    # The endpoint has no authorization since we test via local port directly
    response = urllib.request.urlopen(req)
    data = response.read().decode('utf-8')
    print("AI Chart API Res 1:", data)
except Exception as e:
    print(e)

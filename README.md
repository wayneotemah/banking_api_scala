Bank APIS with Scala

A project implementing scala to recreate banking apis for a mobile banking applications

using Locust to test performance load.
use pip install -r requirements.txt to  install lib. 
run  the following command from the test folder

locust -f load_test.py --headless --users 1000 --spawn-rate 1000 --host=http://127.0.0.1:8081 

Ensure the Bank App is running and cassandra DB is also running
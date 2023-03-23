from locust import HttpUser, task, between
import random
import decimal

class MyScalaUser(HttpUser):
    wait_time = between(0.5, 3)  # add some delay between requests

    @task
    def post_user(self):
        headers = {'Content-type': 'application/json'}
        data = {
                'user': 'user{}'.format(random.randint(1, 1000)),
                'currency': 'KSH',
                'balance': random.randint(0, 10000)
                }
        self.client.post("bank/", json=data, headers=headers)

# locust -f my_test.py --headless --users 1000 --spawn-rate 1000 --host=http://127.0.0.1:8081 run command

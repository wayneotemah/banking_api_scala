from locust import HttpUser, task, between
import random
import decimal


class MyPhpUser(HttpUser):
    wait_time = between(0.5, 3)  # add some delay between requests

    @task
    def post_user(self):
        headers = {'Content-type': 'application/json'}
        data = {
                'user': 'user{}'.format(random.randint(1, 1000)),
                'currency': 'KSH',
                'balance': random.randint(0, 10000),
                'password': "password"
                }
        self.client.post("/user.php", json=data, headers=headers)

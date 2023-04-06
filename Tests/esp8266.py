from locust import HttpUser, task, between
import random
import decimal
home='/'
search = '/search/'
results = '/search/?courses=ACS&campus_choice=1'
# https://www.timetable.kimworks.buzz
class MyScalaUser(HttpUser):
    wait_time = between(0.5, 3)  # add some delay between requests

    @task
    def visit_homepage(self):
        self.client.get(home)

    @task
    def visit_searchpage(self):
        self.client.get(search)

    @task
    def visit_resultpage(self):
        self.client.get(results)
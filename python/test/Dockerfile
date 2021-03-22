FROM python:3.9 as test
WORKDIR /auth_middleware

RUN pip install --upgrade pip
RUN pip install pipenv

COPY auth_middleware/ ./auth_middleware/
COPY Pipfile Pipfile.lock setup.py ./
RUN pipenv install --system --dev

COPY test/ ./test/
COPY resources/ ./resources/

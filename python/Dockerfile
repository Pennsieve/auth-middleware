FROM python:3.9 as build
RUN pip install --upgrade pip
RUN pip install pipenv
WORKDIR /auth_middleware
COPY auth_middleware/ ./auth_middleware/
COPY Makefile Pipfile Pipfile.lock setup.py MANIFEST.in README.md ./
RUN pipenv install --system --dev
RUN python setup.py install
RUN python setup.py bdist_wheel

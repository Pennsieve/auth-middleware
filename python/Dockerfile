FROM python:3.9 as build
RUN pip install --upgrade pip
RUN pip install pipenv
WORKDIR /auth_middleware
COPY auth_middleware/ ./auth_middleware/
COPY Makefile Pipfile Pipfile.lock setup.py MANIFEST.in README.md ./
RUN pipenv install --system --dev
# Built by pip in an isolated environment: the packaging version pinned in
# Pipfile.lock is too old for current setuptools, which breaks setup.py.
RUN pip install --no-deps .
RUN pip wheel --no-deps --wheel-dir dist .

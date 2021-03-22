#!/usr/bin/env/ python
# -*- coding: utf-8 -*-

import re
from setuptools import setup, find_packages

requirements = ["PyJWT", "dataclasses-json"]

with open('auth_middleware/version.py', 'r') as f:
    version = re.search(r'^__version__\s*=\s*[\'"]([^\'"]*)[\'"]',
                        f.read(), re.MULTILINE).group(1)

setup(
    name="auth_middleware",
    version=version,
    author="University of Pennsylvania",
    author_email="peter@pennsieve.com",
    description="Tool for generating JWT tokens for Pennsieve Platform (internal only)",
    packages=find_packages(),
    package_dir={"auth_middleware": "auth_middleware"},
    install_requires=requirements,
    license="",
    classifiers=["Development Status :: 3 - Alpha", "Topic :: Utilities"],
)

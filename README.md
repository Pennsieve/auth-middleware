# Authorization Middleware

## Overview

A library responsible for encoding/decoding JWT's and providing utilitily methods for checking
various permissions related to the JWT claim.

## Claim Structure

A claim is required, at a minimum, to contain roles. These roles encapsulate permissioning
information. The following are two examples of valid claims.

#### User Claim

```javascript
{
    "id": <id>,
    "roles": [
      {
        "id": <id>,
        "role": "owner",
        "type": "organization_role"
      },
      {
        "id": <id>,
        "role": "editor",
        "type": "dataset_role"
      }
    ],
    "type": "user_claim",
    "exp": 1534431697,
    "iat": 1534429897
  }
```

#### Service Claim

```javascript
{
  "roles": [
    {
      "id": "*",
      "role": "owner",
      "type": "organization_role"
    }
  ],
  "type": "service_claim",
  "exp": 1534431697,
  "iat": 1534429897
}
```

## Releasing a new version

Run `make release` to publish a new release of `auth-middleware` to Nexus

## Differences between Python and Scala libraries

The Scala version is primarily functional in design, while the Python version is primarily object-oriented. For example, `hasDatasetAccess(Claim, DatasetId, Permission)` is a function in the Scala library. The corrosponding method in Python is `Claim.has_dataset_access(DatasetId, Permission)`. This pattern is replicated across all of the Python library's implementation.

Some Scala functions take just one object as an argument. These are mirrored as properties on  the corresponding Python classes. For example, `getHeadDatasetIdFromClaim(Claim)` is equivalent to `Claim.head_dataset_id`.

#### Encoding and Decoding Claims
The API for encoding and decoding claims is slightly different in Python.

`Claim.from_token(str, JwtConfig)` is a class method used to create a `Claim` object from a JWT token. It takes a `JwtConfig` as an argument, which contains the `algorithm` (default: HS256), and the secret key.

`Claim.encode(JwtConfig)` is a method on each `Claim` object, and returns the encoded token.

#### Models

The Pennsieve models used by `auth-middleware` have been transcribed to Python as enums, and are located in `models.py`. The values have been transformed from `PascalCase` to `CAPITALIZED_SNAKE_CASE`.



### Example Usage

```python
from auth_middleware import Claim, JwtConfig
from auth_middelware.models import FeatureFlag

config = JwtConfig("secret")

claim = Claim.from_token("...", config)
if claim.has_feature_enabled(OrganizationId(1), FeatureFlag.SOME_FEATURE):
  do_stuff()
else:
  do_other_stuff()
```

## Installation from Nexus
The Python library can be installed with pip from the private PyPI repository
on Nexus using:

```bash
pip install auth-middleware --extra-index-url "https://$PENNSIEVE_NEXUS_USER:$PENNSIEVE_NEXUS_PW@nexus.pennsieve.cc/repository/pypi-prod/simple"
```

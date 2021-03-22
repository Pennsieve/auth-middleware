## Installation

Adding to requirements.txt can be done with git

git+ssh://git@github.com/Pennsieve/auth-middelware@<version>

Clone the repo:

```bash
git clone git@github.com:Pennsieve/auth-middleware.git
```

Install library:

```bash
make install
```

After installation, you should be able to import the library

```python
import auth_middleware
```

## Testing
Run all unit tests:

```bash
make test
```

## Publishing

Run the following in the root of the directory:

```bash
make release
```

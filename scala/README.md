## CLI

You can generate a token for testing locally by running

```bash
$ bin/make-jwt.sh --help
Generate a JWT
  -a, --all-features                           If given, all features will be
                                               included in the JWT
  -d, --dataset  <dataset>                     Integer dataset ID
      --datasetNode  <datasetNode>             Dataset node ID
  -e, --expires  <expires>                     Expiration time in minutes
  -f, --feature  <feature>...                  A feature to add; multiple
                                               features can be specified:
                                               --feature="foo" --feature="bar"
  -k, --key  <key>                             The token encryption key
  -o, --organization  <organization>           Integer organization ID | *
      --organizationNode  <organizationNode>   Organization node ID
  -r, --role  <role>                           The user role. If omitted,
                                               'Viewer' will be used
  -u, --user  <user>                           Integer user ID
  -v, --verbose                                Enables verbose output
  -h, --help                                   Show help message
```

### Example

```bash
$ bin/make-jwt.sh --key="$JWT_ENCRYPTION_KEY" --organization=5 --user=74
eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE1NTc0MjEwNTUsImlhdCI6MTU1NzQxNzQ1NSwiaWQiOjc0LCJyb2xlcyI6W3siaWQiO...
```

## Publishing

### Locally for testing

```bash
$ sbt +publishLocal
```

will build and deploy the jar to your local maven repository.

### Release

Releases of `auth-middleware` are triggered manually. Running

```bash
$ make release
```

and following the prompts will publish the Scala jars. To release both the Python and Scala code, run

````bash
$ make release
````

from the root directory of the repository.

For more information on the release process, see
https://blackfynn.atlassian.net/wiki/spaces/PLAT/pages/743178258/Publishing+JARs

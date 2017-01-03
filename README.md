# Atomist 'travis-rug-type'

[![Build Status](https://travis-ci.org/atomist-rugs/travis-rug-type.svg?branch=master)](https://travis-ci.org/atomist-rugs/travis-rug-type)
[![Slack Status](https://join.atomist.com/badge.svg)](https://join.atomist.com)

Rug extension type for Travis CI.

## Developing

To build and test this project:

```
$ mvn test
```

### Releasing

You must update the version in `src/main/typescript/package.json` to
the version you want released.  Then create a tag whose name is that version.
Push the tag and Travis CI should build and release.

### Updating rug dependency

When you upgrade the rug dependency, you must change it in two places:

-   pom.xml
-   src/main/typescript/package.json

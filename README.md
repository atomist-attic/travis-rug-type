# Atomist 'travis-rug-type'

[![Build Status](https://travis-ci.org/atomist-rugs/travis-rug-type.svg?branch=master)](https://travis-ci.org/atomist-rugs/travis-rug-type)
[![Slack Status](https://join.atomist.com/badge.svg)](https://join.atomist.com)

Rug extension type for Travis CI.

## Using

To gain access to this language extension in your Rug Archive, add to `.atomist/manifest.yml`:

```
extensions:
  - 'com.atomist:travis-rug-type:0.8.0'
```

See [Travis editors](https://github.com/atomist-rugs/travis-editors) for an example.

## Developing

To build and test this project:

```
$ mvn test
```

### Updating rug dependency

To update the rug dependency, change `rug.version` in the pom.xml.

### Releasing

To create a new release of the project, simply push a tag of the form
`M.N.P` where `M`, `N`, and `P` are integers that form the next
appropriate [semantic version][semver] for release.  For example:

```sh
$ git tag -a 1.2.3
```

The Travis CI build (see badge at the top of this page) will
automatically create a GitHub release using the tag name for the
release and the comment provided on the annotated tag as the contents
of the release notes.  It will also automatically upload the needed
artifacts.

[semver]: http://semver.org

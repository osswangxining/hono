A Docker image for building Qpid Proton C and Qpid Dispatch Router binaries.

# Running

The image can be run as a standard Docker container:

    $ docker run -it --rm -v /tmp:/binaries elipsehono/qpid-dispatch-build

**NB** The bind mount of the `/binaries` volume is mandatory. It is used to define the target folder where the build artifacts are being copied to. It must be mapped to an existing local folder.

The image does the following when run:

1. Download the *Proton* and *Dispatch Router* source code artifacts from the Apache Qpid site and extract them.
2. Build the *Proton* library and create the `/binaries/qpid-proton-binary.tar.gz` artifact.
3. Build the *Dispatch Router* and create the `/binaries/qpid-dispatch-binary.tar.gz` artifact.

After the container exits, the binaries can be found in the folder used for the bind mount. In the example above, this would be the `/tmp` folder.

# Build Parameters

The image supports the following environment variables to customize the build:

* `PROTON_VER` - the version of the *Proton* library to build (defaults to `0.15.0`)
* `DISPATCH_VER` - the version of the *Dispatch Router* to build (defaults to `0.7.0`)

**NB** Not all versions of *Dispatch Router* are compatible with each version of the *Proton* library. Please consult the [Apache Qpid project documentation](https://qpid.apache.org) for details regarding compatibility. 
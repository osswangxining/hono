# gets the latest Fedora from Docker Hub
FROM fedora:latest

# required dependencies
RUN dnf -y install gcc gcc-c++ make cmake libuuid-devel openssl-devel \
                   cyrus-sasl-devel cyrus-sasl-plain cyrus-sasl-gssapi cyrus-sasl-md5 \
                   swig python-devel \
                   wget tar

# output directory with binaries for Qpid Proton and Qpid Dispatch Router
ENV OUTDIR /binaries

# make the working directory for building, copying needed files
RUN mkdir working
ADD build_tarball /working
ADD Makefile /working

WORKDIR /working
CMD make all

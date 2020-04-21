FROM gradle:jdk14
COPY --chown=gradle:gradle . /home/gradle
WORKDIR /home/gradle
ENTRYPOINT ["gradle"]
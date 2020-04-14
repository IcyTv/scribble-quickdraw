# scribble-quickdraw

![Run JUnit5 tests](https://github.com/IcyTv/scribble-quickdraw/workflows/Run%20JUnit5%20tests/badge.svg?branch=master)

A Scribble.io clone with [google quickdraw](https://github.com/googlecreativelab/quickdraw-dataset) trained AI integration

## Building and developing

To get started run `gradlew initAll`. This will install npm and gulp for you. To build the frontend files, run `gradlew gulp_default` and to build the Backend (Server) files, run `gradlew build`. In order to run the application, just execute `gradlew run`. To do both, just combine the commands: `gradlew gulp_default run`

If you want to have continuous deployment you can use the following commands:

* `gradlew watch` to watch frontend files for changes and build accordingly
* `gradlew watchAll` to watch frontend files for changes and run the backend server (this behaviour will change in the future to also watch the java files for changes)
* `gradlew devFront` to watch frontend files for changes and start a browserSync instance for easy testing

You can also execute all valid gulp tasks by using `gradlew gulp_<taskname>`

### Sidenote

You need a postgres instance to be running and setup with tables. In order to customize this, just add the postgres data in the [`SQLConnection.java` file](src/main/java/de/icytv/scribble/sql/SQLConnection.java) or in the [`Constants` file](src/main/java/de/icytv/scribble/utils/Constants.java, "WIP to refactor constants here")

## JWT Key generation

This server uses RSA-keys to sign and verify tokens. These are not included in the project for obvious reasons. In order to generate them yourself, create a `keys` folder in the server root folder.
Then make sure you have [openssl](https://www.openssl.org/) installed and run these commands:

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -outform PEM -out public_key.pem
openssl pkcs8 -topk8 -inform PEM -in private.pem -out private_key.pem -nocrypt
```

Filenames `public_key.pem` and `private_key.pem` are mandatory, in order for Java to find the files. You can change the file names in the source code

Keep your keys private, because the private key is *unencrypted*.

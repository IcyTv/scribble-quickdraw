# scribble-quickdraw

A Scribble.io clone with [google quickdraw](https://github.com/googlecreativelab/quickdraw-dataset) trained AI integration

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

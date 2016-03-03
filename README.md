# rapipago_api

JSON API to search rapipago (http://www.rapipago.com) stores

## Prerequisites

1. Install elasticsearch and have it running in localhost:9200,
   or provide the env var `BONSAI_URL` to connect to it

## Running

To start a web server for the application, run:

    lein ring server

## Import stores for bounding box search

    lein run -m rapipago-api.import-stores

## License

Copyright © 2014 Nicolás Berger

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

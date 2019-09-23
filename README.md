# ORI API

Serves the REST Linked Data API for [Open Raadsinformatie](https://github.com/openstate/open-raadsinformatie/), available at `http://id.openraadsinformatie.nl`. There are other APIs available for this dataset as well ([docs](https://docs.openraadsinformatie.nl)).

This program listens to a a kafka topic and converts [incoming events](https://github.com/ontola/linked-delta) into a
serveable file system hierarchy. Multiple serialization formats and a zip file containing them all is generated for
every incoming resource.

Also included in the `nginx` folder is a basic configuration and dockerfile to serve the partitioned folder structure.

Due to their concise and easy syntax, the [n-quads serializations](https://www.w3.org/TR/n-quads/) are considered canon.
These are also used to determine intra-version state changes, any differences between the other formats are ignored.

## Licence
ORI API
Copyright (C) 2019, Argu BV

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

# ORI API

This program listens to a a kafka topic and converts [incoming events](https://github.com/ontola/linked-delta) into a
serveable file system hierarchy. Multiple serialization formats and a zip file containing them all is generated for
every incoming resource.

Also included in the `nginx` folder is a basic configuration and dockerfile to serve the partitioned folder structure.

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

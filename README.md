# Gifdex

A local gif indexing program. You've got a directory full of gifs! This is a
little web app that helps you browse and tag them. It stores the tags in a
local edn file. It's a giant hack and is presented completely as-is.

![doc/screenshot.jpg](A screenshot of the user interface showing gifs).

## Usage

You'll need [Leiningen](https://leiningen.org/) and a JVM.

`lein run wherever/gifs/are` will launch a web server on port 6549 by default.
Pass an optional port as a second argument to change that. Open http://localhost:6549/ and enjoy gifs!

A single image is focused at the top of the screen. Left/right/up/down arrows
change which gif is focused, or you can click a thumbnail.

You can search tags in the text bar at the top of the screen. The special tag
"untagged" finds images without any tags. Searching for nothing returns a
random assortment of gifs from your collection.

To edit the tags for the focused image, click the text bar below the focused
image.

If you tab out of the tag editing bar, it'll automatically focus the next
image, and drop your input into the tag editor. Helpful for bulk tag entry.

## License

Copyright © 2019 Kyle Kingsbury

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

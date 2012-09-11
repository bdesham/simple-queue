# simple-queue

Process a queue of tasks, waiting in between.

## Usage

1. Add `[com.github.bdesham/simple-queue "0.5.1"]` to your `:dependencies` vector.
2. Add `(:use [com.github.bdesham/simple-queue :as q])` to your library’s `ns` macro.
3. Instantiate a queue with `(def my-queue (q/new-queue f))`.
4. Add items with `(q/process queue item)` or `(q/add queue item)`.

## Details

This library maintains queues of objects. Each queue has an associated function `f`: at some periodic interval (one second by default), the function `f` is called with the next queue item as its argument. This function is specified when you create the queue:

```clj
(def my-queue (q/new-queue println))
```

This will create a queue that will print each successive item. You can also specify how long the queue should wait before processing the next item. Pass a number of seconds as the `:delaytime` argument to `new-queue`:

```clj
(def my-queue (q/new-queue println :delaytime 60))
```

This will create the same queue as before but with a one-minute delay between invocations of `f`. The delay time need not be an integer; to wait only 500 milliseconds between actions, use

```clj
(def my-queue (q/new-queue println :delaytime 0.5))
```

There are two functions you can use to add items to the queue. The first, `process`, blocks until the passed item has been processed:

```clj
(def my-queue (q/new-queue f))
(q/process my-queue 6174) ; returns (f 6174), but may take a while to do so
```

The other function, `add`, returns immediately but discards the result of calling `f` with the value, and so this is better suited for cases where `f` has some kind of side effect.

```clj
(def my-queue (q/new-queue println))
(q/add my-queue "Hi there!") ; returns immediately and prints later
```

The library also contains a function `cancel` that stops processing the queue. If an action is already in progress then it is allowed to complete.

```clj
(q/cancel my-queue)
```

## Example

Suppose we want to fetch a bunch of web pages, but in order to avoid flooding the server we’ll wait 30 seconds between requests. In this case let’s suppose that the items we’re putting into the queue are the URLs of the pages we want. This is easily done:

```clj
(def url-queue (q/new-queue slurp :delaytime 30))
(def github (q/process url-queue "https://github.com"))
(def google (q/process url-queue "http://www.google.com"))
```

In this case, `github` will contain the HTML source of the GitHub front page, and likewise for `google`, but there will be a 30-second pause between the two requests.

For a more complicated example, suppose that we want to add our URLs asynchronously and go back to work. In this case we want to store the downloaded HTML somewhere so that it can be processed later.

```clj
(defn cache-url
  [{url :url, filename :filename} item]
  (spit (java.io.File. filename)
        (slurp url)))

(def url-queue (q/new-queue cache-url :delaytime 30))
(q/add url-queue {:url "https://github.com",
                  :filename "github.html"})    ; returns immediately
(q/add url-queue {:url "https://google.com",
                  :filename "google.html"})    ; returns immediately
```

Now execution continues immediately after the two items are added to the queue, and we’ll see the files “github.html” and “google.html” populated slowly as the queue executes.

## Caveats

This library hasn’t been tested very extensively yet! Use pre-1.0 versions at your own risk.

The desired behavior of the queue is that the first task is executed immediately upon being added to the queue. This seems to be the case, but testing is required to make sure that the *second* item isn’t being processed too quickly.

## License

Copyright © 2012 Benjamin D. Esham (www.bdesham.info).

This project is distributed under the Eclipse Public License, the same as that used by Clojure. A copy of the license is included as “epl-v10.html” in this distribution.

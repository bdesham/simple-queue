(ns com.github.bdesham.simple-queue
  (:import (java.util.concurrent Executors TimeUnit)))

(defn new-queue
  "Creates a new queue. Each trigger from the timer will cause the function f
  to be invoked with the next item from the queue. The queue begins processing
  immediately, which in practice means that the first item to be added to the
  queue is processed immediately.

  Options
  - delaytime: time between invocations (in seconds). Defaults to one second.

  Example usage:

  ; Every second, print the value of the next item in the queue
  (def my-queue (q/new-queue println))

  ; Every minute, print the value of the next item in the queue
  (def my-queue (q/new-queue println :delaytime 60))

  ; If the items in the queue are functions that take no arguments, use this to
  ; execute one function every 500 milliseconds
  (def my-queue (q/new-queue #(%) :delaytime 0.5))"
  [f & opts]
  (let [options (into {:delaytime 1}
                      (select-keys (apply hash-map opts) [:delaytime])),
        delaytime (:delaytime options),
        queue {:queue (java.util.concurrent.LinkedBlockingQueue.)},
        func #(let [item (.take (:queue queue)),
                    value (:value item),
                    prom (:promise item)]
                (if prom
                  (deliver prom (f value))
                  (f value))),
        pool (Executors/newScheduledThreadPool 1),
        task (.scheduleWithFixedDelay pool func
                                      0 (long (* 1000 delaytime))
                                      TimeUnit/MILLISECONDS)]
    (assoc queue :task task)))

(defn cancel
  "Permanently stops execution of the queue. If a task is already executing
  then it proceeds unharmed."
  [queue]
  (.cancel (:timer queue)))

(defn process
  "Adds an item to the queue, blocking until it has been processed. Returns
  (f item)."
  [queue item]
  (let [prom (promise)]
    (.offer (:queue queue)
            {:value item,
             :promise prom})
    @prom))

(defn add
  "Adds an item to the queue and returns immediately. The value of (f item) is
  discarded, so presumably f has side effects if you're using this."
  [queue item]
  (.offer (:queue queue)
          {:value item,
           :promise nil}))

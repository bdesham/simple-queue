(ns com.github.bdesham.simple-queue
  (:import (java.util.concurrent Executors TimeUnit LinkedBlockingQueue)))

(defn new-queue
  "Creates a new queue. Each trigger from the timer will cause the function f
  to be invoked with the next item from the queue. The queue begins processing
  immediately, which in practice means that the first item to be added to the
  queue is processed immediately.

  Options
  - delay: time in seconds between invocations. Defaults to one second. May be
    a non-integer value.

  Example usage:

  ; Every second, print the value of the next item in the queue
  (def my-queue (q/new-queue println))

  ; Every minute, print the value of the next item in the queue
  (def my-queue (q/new-queue println :delay 60))

  ; If the items in the queue are functions that take no arguments, use this to
  ; execute one function every 500 milliseconds
  (def my-queue (q/new-queue #(%) :delay 0.5))"
  [f & opts]
  (let [options (apply hash-map opts),
        delaytime (get options :delay 1),
        queue {:queue (LinkedBlockingQueue.)},
        func #(let [item (.take (:queue queue)),
                    value (:value item),
                    prom (:promise item)]
                (if prom
                  (deliver prom (f value))
                  (f value))),
        scheduler (delay (let [pool (Executors/newScheduledThreadPool 1),
                               task (.scheduleWithFixedDelay pool func
                                                             0 (long (* 1000 delaytime))
                                                             TimeUnit/MILLISECONDS)]
                           {:pool pool, :task task}))]
    (assoc queue :scheduler scheduler)))

(defn cancel
  "Permanently stops execution of the queue. If a task is already executing
  then it may or may not finish. If the queue is cancelled while it's delayed
  waiting for the next item to be processed, that item will never be
  processed."
  [queue]
  (.shutdownNow (:pool (deref (:scheduler queue))))
  nil)

(defn process
  "Adds an item to the queue, blocking until it has been processed. Returns (f
  item). If the queue has been cancelled, returns nil."
  [queue item]
  (when-not (.isShutdown (:pool (deref (:scheduler queue))))
    (let [prom (promise)]
      (.offer (:queue queue)
              {:value item,
               :promise prom})
      @prom)))

(defn add
  "Adds an item to the queue and returns immediately. The value of (f item) is
  discarded, so presumably f has side effects if you're using this. Returns
  true if the item was added, or false if the queue has been cancelled."
  [queue item]
  (when-not (.isShutdown (:pool (deref (:scheduler queue))))
    (.offer (:queue queue)
            {:value item,
             :promise nil})))

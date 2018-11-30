(ns mach.pack.alpha.impl.elodin
  "Master namer elodin will fulfill your naming needs.

  Provides functionality for naming a file suitable for use in archives."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string])
  (:import
   [java.nio.file Files Paths]))

(defn- signature
  [algorithm]
  (javax.xml.bind.DatatypeConverter/printHexBinary (.digest algorithm)))

(defn- consume-input-stream
  [input-stream]
  (let [buf-n 2048
        buffer (byte-array buf-n)]
    (while (> (.read input-stream buffer 0 buf-n) 0))))

(defn- sha256
  [file]
  (.getMessageDigest
    (doto
      (java.security.DigestInputStream.
        (io/input-stream file)
        (java.security.MessageDigest/getInstance "SHA-256"))
      consume-input-stream)))

(defn hash-derived-name
  [file]
  [(str (signature (sha256 file)) "-" (.getName file))])

(defn- paths-get
  [[first & more]]
  (Paths/get first (into-array String more)))

(defn path-seq->str
  [path-seq]
  (str (paths-get path-seq)))

(defn path->path-seq
  [path]
  (->> path
       .iterator
       iterator-seq
       (map str)))

(defn file->path-seq
  [file]
  (let [p (.toPath (io/file file))]
    (concat
      (some-> (.getRoot p) str vector)
      (->> p
           .iterator
           iterator-seq
           (map str)))))

(defn str->path
  [pstr]
  (.toPath (io/file pstr)))

(defn str->abs-path
  [pstr]
  (.toPath (.getCanonicalFile (io/file pstr))))

(defn full-path-derived-name
  [file]
  (file->path-seq file))

(defn versioned-lib
  [{:keys [lib] :as all}]
  (format "%s__%s__%s"
          (namespace lib)
          (name lib)
          ((some-fn :mvn/version :sha) all)))

(defn directory-unique
  [{:keys [path deps/root]}]
  (string/join
    "-"
    (path->path-seq
      (.relativize (str->abs-path root)
                   (str->path path)))))

(defn jar-name
  [all]
  (str (versioned-lib all) ".jar"))

(defn directory-name
  [all]
  (str (versioned-lib all) "-" (directory-unique all)))

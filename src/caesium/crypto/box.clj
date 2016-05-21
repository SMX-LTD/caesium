(ns caesium.crypto.box
  "Bindings to the public key authenticated encryption scheme."
  (:require [caesium.binding :refer [defconsts sodium]]
            [caesium.crypto.scalarmult :as s]))

(defconsts [seedbytes
            publickeybytes
            secretkeybytes
            noncebytes
            macbytes
            primitive])

(defn keypair-to-buf!
  "Generate a key pair into provided pk (public key) and sk (secret
  key) bufs. If also passed a seed, uses it to seed the key pair.

  This API matches libsodium's `crypto_box_keypair` and
  `crpyto_box_seed_keypair`."
  ([pk sk]
   (.crypto_box_keypair sodium pk sk))
  ([pk sk seed]
   (.crypto_box_seed_keypair sodium pk sk seed)))

(defn keypair!
  "Create a `crypto_box` keypair.

  This fn will take either:

  - nothing, generating the key pair from scratch securely
  - a seed, generating the key pair from the seed

  Previously, this API matched Kalium, where the seed would be used as the
  secret key directly. Now, it matches libsodium, where the seed is hashed
  before being used as a secret. The old behavior can be useful in some cases,
  e.g. if you are storage-constrained and only want to store secret keys, and
  you care that it is _really_ the secret key and not some value derived from
  it (you probably don't). See [[sk->keypair]] for details.

  Returns a map containing the public and private key bytes (mutable
  arrays)."
  ([]
   (let [pk (byte-array publickeybytes)
         sk (byte-array secretkeybytes)]
     (keypair-to-buf! pk sk)
     {:public pk :secret sk}))
  ([seed]
   (let [pk (byte-array publickeybytes)
         sk (byte-array secretkeybytes)]
     (keypair-to-buf! pk sk seed)
     {:public pk :secret sk})))

(def ^:deprecated generate-keypair
  "Deprecated alias for [[keypair!]].

  Please note that there was a breaking backwards-incompatible change between
  0.4.0 and 0.5.0+ if you specify a seed; see [[keypair!]] docs for details."
  keypair!)

(defn sk->keypair
  "Generates a key pair from a secret key.

  This is different from generating a key pair from a seed. The former
  uses the libsodium API which will first hash the secret to an array
  of appropriate length; this will use the secret key verbatim. To be
  precise: it will use the secret key as a scalar to perform the
  Curve25519 scalar mult."
  [sk]
  (let [pk (byte-array publickeybytes)]
    (s/scalarmult-to-buf! sk pk)
    {:public pk :secret sk}))

(defn box-easy-to-buf!
  "Encrypts ptext into out with `crypto_box_easy` using given nonce,
  public key and secret key.

  This function is only useful if you're managing your own output
  buffer, which includes in-place encryption. You probably
  want [[box-easy]]."
  [out ptext nonce pk sk]
  (let [plen (alength ^bytes ptext)]
    (.crypto_box_easy sodium out ptext plen nonce pk sk)
    out))

(defn box-open-easy-to-buf!
  "Decrypts ptext into out with `crypto_box_open_easy` using given
  nonce, public key and secret key.

  This function is only useful if you're managing your own output
  buffer, which includes in-place decryption. You probably
  want [[box-open-easy]]."
  [out ctext nonce pk sk]
  (let [clen (alength ^bytes ctext)
        res (.crypto_box_open_easy sodium out ctext clen nonce pk sk)]
    (if (zero? res)
      out
      (throw (RuntimeException. "Ciphertext verification failed")))))

(defn mlen->clen
  "Given a plaintext length, return the ciphertext length.

  This should be an implementation detail unless you want to manage
  your own output buffer together with [[box-easy-to-buf!]]."
  [mlen]
  (+ mlen macbytes))

(defn clen->mlen
  "Given a ciphertext length, return the plaintext length.

  This should be an implementation detail unless you want to manage
  your own output buffer together with [[box-open-easy-to-buf!]]."
  [clen]
  (- clen macbytes))

(defn box-easy
  "Encrypts ptext with `crypto_box_easy` using given nonce, public key
  and secret key.

  This creates the output ciphertext byte array for you, which is
  probably what you want. If you would like to manage the array
  yourself, or do in-place encryption, see [[box-easy-to-buf!]]."
  [ptext nonce pk sk]
  (let [out (byte-array (mlen->clen (alength ^bytes ptext)))]
    (box-easy-to-buf! out ptext nonce pk sk)))

(defn box-open-easy
  "Decrypts ptext with `crypto_box_open_easy` using given nonce, public
  key and secret key.

  This creates the output plaintext byte array for you, which is probably what
  you want. If you would like to manage the array yourself, or do in-place
  decryption, see [[box-open-easy-to-buf!]]."
  [ctext nonce pk sk]
  (let [out (byte-array (clen->mlen (alength ^bytes ctext)))]
    (box-open-easy-to-buf! out ctext nonce pk sk)))

(defn encrypt
  "Encrypt with `crypto_box_easy`.

  To encrypt, use the recipient's public key and the sender's secret
  key.

  This is an alias for [[box-easy]] with a different argument
  order. [[box-easy]] follows the same argument order as the libsodium
  function."
  [pk sk nonce ptext]
  (box-easy ptext nonce pk sk))

(defn decrypt
  "Decrypt with `crypto_box_open_easy`.

  To decrypt, use the sender's public key and the recipient's secret
  key.

  This is an alias for [[box-open-easy]] with a different argument
  order. [[box-open-easy]] follows the same argument order as the
  libsodium function."
  [pk sk nonce ctext]
  (box-open-easy ctext nonce pk sk))
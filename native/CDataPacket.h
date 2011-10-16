/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_labs_rpc_CDataPacket */

#ifndef _Included_com_labs_rpc_CDataPacket
#define _Included_com_labs_rpc_CDataPacket
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    packObject
 * Signature: (Ljava/lang/Object;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_labs_rpc_CDataPacket_packObject
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    unpackObject
 * Signature: (Ljava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_com_labs_rpc_CDataPacket_unpackObject
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    makeHeaderBytes
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_labs_rpc_CDataPacket_makeHeaderBytes
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    makePacketBytes
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_labs_rpc_CDataPacket_makePacketBytes
  (JNIEnv *, jobject, jbyteArray, jbyteArray);

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    getBytes
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_labs_rpc_CDataPacket_getBytes
  (JNIEnv *, jobject);

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    fromBytes
 * Signature: ([B)Lcom/labs/rpc/CDataPacket;
 */
JNIEXPORT jobject JNICALL Java_com_labs_rpc_CDataPacket_fromBytes
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    fromStream
 * Signature: (Ljava/io/InputStream;)Lcom/labs/rpc/CDataPacket;
 */
JNIEXPORT jobject JNICALL Java_com_labs_rpc_CDataPacket_fromStream
  (JNIEnv *, jobject, jobject);

#ifdef __cplusplus
}
#endif
#endif
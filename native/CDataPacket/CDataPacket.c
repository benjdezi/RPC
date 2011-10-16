#include <stdio.h>
#include <string.h>
#include "CDataPacket.h"

#define NULL_VALUE = "null";				// Null value
#define HEADER_SIZE = 21;					// Size of the header
#define FORMAT_NULL = 0x40;					// Null
#define FORMAT_BOOL = 0x41;					// Boolean
#define FORMAT_BYTE = 0x42;					// Byte (0-255)
#define FORMAT_CHAR = 0x43;					// Character
#define FORMAT_SHORT = 0x44;				// Short integer
#define FORMAT_INT = 0x45;					// Integer
#define FORMAT_FLOAT = 0x46;				// Float
#define FORMAT_DOUBLE = 0x47;				// Double
#define FORMAT_LONG = 0x48;					// Long
#define FORMAT_STRING = 0x49;				// String
#define FORMAT_ARRAY = 0x50;				// Array of objects
#define FORMAT_LIST = 0x51;					// List of objects
#define FORMAT_JSON = 0x52;					// JSON object
#define FORMAT_JSON_ARRAY = 0x53;			// JSON array
#define FORMAT_REMOTE_EX = 0x54;			// Remote exception

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    packObject
 * Signature: (Ljava/lang/Object;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_labs_rpc_CDataPacket_packObject(JNIEnv *env, jclass cls, jobject obj) {
	return NULL;
}

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    unpackObject
 * Signature: (Ljava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_com_labs_rpc_CDataPacket_unpackObject(JNIEnv *env, jclass cls, jstring objData) {
	return NULL;
}

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    makeHeaderBytes
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_labs_rpc_CDataPacket_makeHeaderBytes(JNIEnv *env, jobject obj, jint payloadSize) {
	return NULL;
}

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    makePacketBytes
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_labs_rpc_CDataPacket_makePacketBytes(JNIEnv *env, jobject obj, jbyteArray headerBytes, jbyteArray payloadBytes) {
	return NULL;
}

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    getBytes
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_labs_rpc_CDataPacket_getBytes(JNIEnv *env, jobject obj) {
	return NULL;
}

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    fromBytes
 * Signature: ([B)Lcom/labs/rpc/CDataPacket;
 */
JNIEXPORT jobject JNICALL Java_com_labs_rpc_CDataPacket_fromBytes(JNIEnv *env, jclass cls, jbyteArray bytes) {
	return NULL;
}

/*
 * Class:     com_labs_rpc_CDataPacket
 * Method:    fromStream
 * Signature: (Ljava/io/InputStream;)Lcom/labs/rpc/CDataPacket;
 */
JNIEXPORT jobject JNICALL Java_com_labs_rpc_CDataPacket_fromStream(JNIEnv *env, jobject obj, jobject stream) {
	return NULL;
}

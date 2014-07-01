/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/kaznog/Documents/DreamNoteWorkSpace/DreamNote/src/com/kaznog/android/dreamnote/smartclip/ClipServiceInterface.aidl
 */
package com.kaznog.android.dreamnote.smartclip;
public interface ClipServiceInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.kaznog.android.dreamnote.smartclip.ClipServiceInterface
{
private static final java.lang.String DESCRIPTOR = "com.kaznog.android.dreamnote.smartclip.ClipServiceInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.kaznog.android.dreamnote.smartclip.ClipServiceInterface interface,
 * generating a proxy if needed.
 */
public static com.kaznog.android.dreamnote.smartclip.ClipServiceInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.kaznog.android.dreamnote.smartclip.ClipServiceInterface))) {
return ((com.kaznog.android.dreamnote.smartclip.ClipServiceInterface)iin);
}
return new com.kaznog.android.dreamnote.smartclip.ClipServiceInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_registerCallback:
{
data.enforceInterface(DESCRIPTOR);
com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface _arg0;
_arg0 = com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface.Stub.asInterface(data.readStrongBinder());
int _arg1;
_arg1 = data.readInt();
this.registerCallback(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_unregisterCallback:
{
data.enforceInterface(DESCRIPTOR);
com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface _arg0;
_arg0 = com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface.Stub.asInterface(data.readStrongBinder());
this.unregisterCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_cancel:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.cancel(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.kaznog.android.dreamnote.smartclip.ClipServiceInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
// 登録

@Override public void registerCallback(com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface callback, int index) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
_data.writeInt(index);
mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
// 登録解除

@Override public void unregisterCallback(com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
// クリップ中止

@Override public void cancel(int index) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(index);
mRemote.transact(Stub.TRANSACTION_cancel, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_cancel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
// 登録

public void registerCallback(com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface callback, int index) throws android.os.RemoteException;
// 登録解除

public void unregisterCallback(com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface callback) throws android.os.RemoteException;
// クリップ中止

public void cancel(int index) throws android.os.RemoteException;
}

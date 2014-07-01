/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/kaznog/Documents/DreamNoteWorkSpace/DreamNote/src/com/kaznog/android/dreamnote/smartclip/ClipServiceCallbackInterface.aidl
 */
package com.kaznog.android.dreamnote.smartclip;
public interface ClipServiceCallbackInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface
{
private static final java.lang.String DESCRIPTOR = "com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface interface,
 * generating a proxy if needed.
 */
public static com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface))) {
return ((com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface)iin);
}
return new com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface.Stub.Proxy(obj);
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
case TRANSACTION_update:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.update(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface
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
@Override public void update(int progress) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(progress);
mRemote.transact(Stub.TRANSACTION_update, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_update = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void update(int progress) throws android.os.RemoteException;
}

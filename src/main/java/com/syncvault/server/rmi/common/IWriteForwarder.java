package com.syncvault.server.rmi.common;

import com.syncvault.server.rmi.dto.WriteOperationRequest;
import com.syncvault.server.rmi.dto.WriteOperationResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Giao diện RMI ủy quyền thao tác ghi từ Server B sang Server A
public interface IWriteForwarder extends Remote {
    WriteOperationResponse executeForwardedWrite(WriteOperationRequest request) throws RemoteException;
}

package com.syncvault.server.rmi.common;

import com.syncvault.server.rmi.dto.ReplicationPayload;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Giao diện RMI đồng bộ tức thời từ Server A (Primary) sang Server B (Secondary)
public interface IReplicationService extends Remote {
    boolean replicateImmediately(ReplicationPayload payload) throws RemoteException;
}

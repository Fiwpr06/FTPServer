package com.syncvault.server.rmi.common;

import com.syncvault.server.rmi.dto.SessionCheckRequest;
import com.syncvault.server.rmi.dto.SessionCheckResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Giao diện RMI điều phối phiên đăng nhập độc quyền trên toàn cụm
public interface ISessionCoordinator extends Remote {
    SessionCheckResponse requestSessionAcquisition(SessionCheckRequest request) throws RemoteException;
    void notifySessionReleased(String username, String serverId) throws RemoteException;
    boolean isUserActive(String username) throws RemoteException;
}

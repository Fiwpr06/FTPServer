package com.syncvault.server.core.command;

import com.syncvault.server.core.command.auth.PassCommand;
import com.syncvault.server.core.command.auth.QuitCommand;
import com.syncvault.server.core.command.auth.UserCommand;
import com.syncvault.server.core.command.directory.CdupCommand;
import com.syncvault.server.core.command.directory.CwdCommand;
import com.syncvault.server.core.command.directory.MkdCommand;
import com.syncvault.server.core.command.directory.PwdCommand;
import com.syncvault.server.core.command.directory.RmdCommand;
import com.syncvault.server.core.command.system.FeatCommand;
import com.syncvault.server.core.command.system.NoopCommand;
import com.syncvault.server.core.command.system.SystCommand;
import com.syncvault.server.core.command.transfer.DeleCommand;
import com.syncvault.server.core.command.transfer.ListCommand;
import com.syncvault.server.core.command.transfer.PasvCommand;
import com.syncvault.server.core.command.transfer.RetrCommand;
import com.syncvault.server.core.command.transfer.StorCommand;
import com.syncvault.server.core.command.transfer.TypeCommand;

import java.util.HashMap;
import java.util.Map;

// Bộ định tuyến lệnh FTP theo chuẩn Command Pattern
public class CommandFactory {
    private final Map<String, FtpCommand> commandMap = new HashMap<>();

    public CommandFactory() {
        // Xác thực
        commandMap.put("USER", new UserCommand());
        commandMap.put("PASS", new PassCommand());
        commandMap.put("QUIT", new QuitCommand());

        // Điều hướng & thư mục
        commandMap.put("PWD", new PwdCommand());
        commandMap.put("XPWD", new PwdCommand());
        commandMap.put("CWD", new CwdCommand());
        commandMap.put("CDUP", new CdupCommand());
        commandMap.put("XCUP", new CdupCommand());
        commandMap.put("MKD", new MkdCommand());
        commandMap.put("XMKD", new MkdCommand());
        commandMap.put("RMD", new RmdCommand());
        commandMap.put("XRMD", new RmdCommand());

        // Truyền tệp
        commandMap.put("PASV", new PasvCommand());
        commandMap.put("TYPE", new TypeCommand());
        commandMap.put("LIST", new ListCommand());
        commandMap.put("NLST", new ListCommand());
        commandMap.put("RETR", new RetrCommand());
        commandMap.put("STOR", new StorCommand());
        commandMap.put("DELE", new DeleCommand());

        // Hệ thống & Bổ trợ
        commandMap.put("SYST", new SystCommand());
        commandMap.put("FEAT", new FeatCommand());
        commandMap.put("NOOP", new NoopCommand());
    }

    public FtpCommand getCommand(String commandName) {
        if (commandName == null) return null;
        return commandMap.get(commandName.trim().toUpperCase());
    }
}

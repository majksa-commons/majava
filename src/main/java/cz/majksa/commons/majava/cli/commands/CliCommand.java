/*
 *  majava - cz.majksa.commons.majava.cli.commands.CliCommand
 *  Copyright (C) 2021  Majksa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cz.majksa.commons.majava.cli.commands;

import cz.majksa.commons.majava.cli.ConsoleTextBuilder;
import lombok.Getter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <p><b>Class {@link CliCommand}</b></p>
 *
 * @author majksa
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
public abstract class CliCommand {

    @Nonnull
    protected final String name;

    @Nonnull
    protected final String route;

    @Nonnull
    protected final String description;

    @Nonnull
    protected final ConsoleTextBuilder consoleMessenger = new ConsoleTextBuilder();

    @Nonnull
    protected final Options options = new Options()
            .addOption("h", "help", false, "shows this message");

    public CliCommand(@Nullable CommandsGroup group, @Nonnull String name, @Nonnull String description) {
        this.name = name;
        if (group == null) {
            route = name;
        } else {
            this.route = group.getName() + ":" + name;
        }
        this.description = description;
    }

    public void run(@Nonnull String[] args) throws ConsoleRuntimeException {
        try {
            final CommandLine commandLine = new DefaultParser().parse(options, args);
            if (commandLine.hasOption('h')) {
                help();
            } else {
                onCommand(commandLine);
            }
        } catch (ParseException e) {
            throw new ConsoleRuntimeException(e);
        }
    }

    protected abstract void onCommand(@Nonnull CommandLine commandLine) throws ConsoleRuntimeException;

    public void help() {
        new HelpFormatter().printHelp(ConsoleTextBuilder.WIDTH, route, null, options, null);
    }

}

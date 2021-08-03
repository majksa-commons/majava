/*
 *  majava - cz.majksa.commons.majava.modules.ModulesStarter
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

package cz.majksa.commons.majava.modules;

import cz.majksa.commons.majava.utils.AsyncUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p><b>Class {@link cz.majksa.commons.majava.modules.ModulesStarter}</b></p>
 *
 * @author majksa
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ModulesStarter {

    @Nonnull
    private final Map<Class<? extends Module<? extends ModuleConfig>>, Module<? extends ModuleConfig>> modules;
    private final Map<Class<? extends Module<? extends ModuleConfig>>, List<Class<? extends Module<? extends ModuleConfig>>>> modulesDependencies = new HashMap<>();

    public ModulesStarter(@Nonnull Modules modules) {
        this.modules = modules.getMap();
    }

    @Nonnull
    public CompletableFuture<Void> start() {
        return CompletableFuture.allOf(
                modules.values()
                        .stream()
                        .map(this::start)
                        .toArray(CompletableFuture[]::new)
        );
    }

    @Nonnull
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.allOf(
                modules.values()
                        .stream()
                        .map(this::shutdown)
                        .toArray(CompletableFuture[]::new)
        );
    }

    @Nonnull
    public CompletableFuture<Void> start(Class<? extends Module<? extends ModuleConfig>> module) {
        return start(modules.get(module));
    }

    @Nonnull
    public CompletableFuture<Void> shutdown(Class<? extends Module<? extends ModuleConfig>> module) {
        return shutdown(modules.get(module));
    }

    @Nonnull
    private CompletableFuture<Void> start(@Nonnull Module<? extends ModuleConfig> module) {
        if (module.isStarted()) {
            return module.getFuture();
        }
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        final List<Class<? extends Module<? extends ModuleConfig>>> dependencies = module.getDependencies();
        for (Class<? extends Module<? extends ModuleConfig>> dependencyClass : dependencies) {
            modulesDependencies.computeIfAbsent(dependencyClass, clazz -> new ArrayList<>());
            modulesDependencies.get(dependencyClass).add(module.getModuleClass());
            final Module<? extends ModuleConfig> dependency = modules.get(dependencyClass);
            if (dependency == null) {
                throw new IllegalArgumentException("Module " + module.name + " depends on " + dependencyClass.getName() + ", which has not been registered!");
            }
            futures.add(start(dependency));
        }
        futures.forEach(CompletableFuture::join);
        return module.start();
    }

    @Nonnull
    private CompletableFuture<Void> shutdown(@Nonnull Module<? extends ModuleConfig> module) {
        if (!module.isStarted()) {
            return module.getFuture();
        }
        modulesDependencies.getOrDefault(module.getModuleClass(), Collections.emptyList())
                .stream()
                .map(modules::get)
                .map(this::shutdown)
                .forEach(CompletableFuture::join);
        return module.shutdown();
    }

}

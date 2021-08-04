/*
 *  majava - cz.majksa.commons.majava.listeners.eventhandlers.EventsHandler
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

package cz.majksa.commons.majava.listeners.eventhandlers;

import cz.majksa.commons.majava.listeners.EntryPoint;
import cz.majksa.commons.majava.listeners.EntryPointList;
import cz.majksa.commons.majava.listeners.IListener;
import cz.majksa.commons.majava.utils.AsyncUtils;
import cz.majksa.commons.majava.utils.ClassUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p><b>Interface {@link cz.majksa.commons.majava.listeners.eventhandlers.EventsHandler}</b></p>
 *
 * @author majksa
 * @version 1.0.0
 * @since 1.0.0
 */
public interface EventsHandler<E> {

    @Nonnull
    Class<E> getRootEvent();

    @Nonnull
    Function<Throwable, Void> getLoggingFunction();

    @Nonnull
    Map<Class<? extends E>, EntryPointList<? extends E>> getEntryPointsMap();

    void start();

    void stop();

    @Nonnull
    default CompletableFuture<Void> runEvent(@Nonnull E event) {
        return CompletableFuture.allOf(
                ClassUtils.getParents(event.getClass(), getRootEvent())
                        .stream()
                        .map(this::get)
                        .map(entryPointList -> run(entryPointList, event))
                        .toArray(CompletableFuture[]::new)
        );
    }

    @Nonnull
    default <T extends E> EntryPoint<T> prepare(@Nonnull Class<T> clazz, @Nonnull Function<T, CompletableFuture<Void>> callback, @Nonnull Predicate<T> predicate) {
        return prepare(get(clazz), clazz, callback, predicate);
    }

    @Nonnull
    default <T extends E> EntryPoint<T> prepare(@Nonnull EntryPointList<T> entryPointList, @Nonnull Class<T> clazz, @Nonnull Function<T, CompletableFuture<Void>> callback, @Nonnull Predicate<T> predicate) {
        return entryPointList.prepare(clazz, callback, predicate);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    default <T extends E> CompletableFuture<Void> run(@Nonnull EntryPointList<T> entryPointList, @Nonnull E event) {
        return CompletableFuture.runAsync(() -> {
            final List<CompletableFuture<Void>> futureList = entryPointList.run((T) event);
            for (CompletableFuture<Void> future : futureList) {
                future.exceptionally(getLoggingFunction()).join();
            }
        });
    }

    /**
     * Find the corresponding {@link cz.majksa.commons.majava.listeners.EntryPointList} by the given class with generics.
     *
     * @param <T>   the type of the event
     * @param clazz the class of the event type
     * @return the {@link cz.majksa.commons.majava.listeners.EntryPointList} corresponding to the given class
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    default <T extends E> EntryPointList<T> get(Class<?> clazz) {
        final Class<? extends E> castedClass = (Class<? extends E>) clazz;
        getEntryPointsMap().putIfAbsent(castedClass, new EntryPointList<>());
        return (EntryPointList<T>) getEntryPointsMap().get(castedClass);
    }

    /**
     * Loads a listener class and converts it into an {@link cz.majksa.commons.majava.listeners.EntryPoint}
     *
     * @param listener the listener to be loaded
     * @param <T>      the event type the listener listens to
     * @return the created {@link cz.majksa.commons.majava.listeners.EntryPoint}
     */
    @Nonnull
    default <T extends E> EntryPoint<T> loadListener(@Nonnull IListener<T> listener) {
        return prepare(listener.getEventClass(), listener::run, listener::check);
    }

}

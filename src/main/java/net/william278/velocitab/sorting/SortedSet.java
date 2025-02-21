/*
 * This file is part of Velocitab, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.velocitab.sorting;

import com.google.common.collect.Maps;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

public class SortedSet {
    private final ConcurrentSkipListSet<String> sortedTeams;
    private final Map<String, Integer> positionMap;

    public SortedSet(Comparator<String> comparator) {
        sortedTeams = new ConcurrentSkipListSet<>(comparator);
        positionMap = Maps.newConcurrentMap();
    }

    public boolean addTeam(String teamName) {
        if (!sortedTeams.add(teamName)) {
            return false;
        }
        updatePositions(teamName);
        return true;
    }

    public boolean removeTeam(String teamName) {
        if (!sortedTeams.remove(teamName)) {
            return false;
        }
        updatePositions(null);
        return true;
    }

    private void updatePositions(String newTeam) {
        if (newTeam != null) {
            int newPosition = sortedTeams.headSet(newTeam).size();
            positionMap.put(newTeam, newPosition);
            sortedTeams.tailSet(newTeam).forEach(team -> positionMap.put(team, sortedTeams.headSet(team).size()));
        } else {
            int index = 0;
            positionMap.clear();
            for (String team : sortedTeams) {
                positionMap.put(team, index++);
            }
        }
    }

    public int getPosition(String teamName) {
        return positionMap.getOrDefault(teamName, -1);
    }

    @Override
    public String toString() {
        return sortedTeams.toString();
    }
}

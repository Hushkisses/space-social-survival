package com.hushkisses.spacesurvival.role.selection;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.DefaultRoleCatalog;
import com.hushkisses.spacesurvival.role.RoleId;
import com.hushkisses.spacesurvival.role.RoleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleSelectionServiceTest {

    @Test
    void generatesThreeDistinctCandidatesPerPlayer() {
        RoleSelectionService service = new RoleSelectionService(
                DefaultRoleCatalog.createRegistry()
        );

        List<PlayerId> players = players(6);
        service.prepareCandidates(players, new Random(42));

        for (PlayerId player : players) {
            RoleCandidateSet set = service.candidates(player).orElseThrow();
            assertEquals(3, set.candidates().size());
            assertEquals(3, set.candidates().stream().distinct().count());
        }
    }

    @Test
    void sameSeedReproducesSameCandidates() {
        List<PlayerId> players = players(6);

        RoleSelectionService first = new RoleSelectionService(
                DefaultRoleCatalog.createRegistry()
        );
        RoleSelectionService second = new RoleSelectionService(
                DefaultRoleCatalog.createRegistry()
        );

        first.prepareCandidates(players, new Random(12345));
        second.prepareCandidates(players, new Random(12345));

        for (PlayerId player : players) {
            assertEquals(
                    first.candidates(player).orElseThrow().candidates(),
                    second.candidates(player).orElseThrow().candidates()
            );
        }
    }

    @Test
    void playerCanSelectOnlyOfferedRole() {
        RoleSelectionService service = new RoleSelectionService(
                DefaultRoleCatalog.createRegistry()
        );
        PlayerId player = player(1);
        service.prepareCandidates(List.of(player), new Random(5));

        RoleCandidateSet candidates = service.candidates(player).orElseThrow();
        RoleId offered = candidates.candidates().getFirst();
        RoleId notOffered = DefaultRoleCatalog.createDefinitions().stream()
                .map(definition -> definition.id())
                .filter(id -> !candidates.contains(id))
                .findFirst()
                .orElseThrow();

        assertEquals(
                RoleSelectionResult.ROLE_NOT_OFFERED,
                service.select(player, notOffered)
        );
        assertEquals(
                RoleSelectionResult.SELECTED,
                service.select(player, offered)
        );
    }

    @Test
    void playerCannotSelectTwice() {
        RoleSelectionService service = new RoleSelectionService(
                DefaultRoleCatalog.createRegistry()
        );
        PlayerId player = player(1);
        service.prepareCandidates(List.of(player), new Random(3));

        RoleId role = service.candidates(player).orElseThrow()
                .candidates().getFirst();

        assertEquals(RoleSelectionResult.SELECTED, service.select(player, role));
        assertEquals(
                RoleSelectionResult.ALREADY_SELECTED,
                service.select(player, role)
        );
    }

    @Test
    void roleCopyLimitIsEnforced() {
        RoleRegistry registry = DefaultRoleCatalog.createRegistry();
        RoleSelectionService service = new RoleSelectionService(registry);

        List<PlayerId> players = players(10);
        service.prepareCandidates(players, new Random(1));

        RoleId targetRole = null;
        PlayerId first = null;
        PlayerId second = null;
        PlayerId third = null;

        for (RoleId roleId : registry.all().stream().map(r -> r.id()).toList()) {
            List<PlayerId> offered = players.stream()
                    .filter(p -> service.candidates(p).orElseThrow().contains(roleId))
                    .toList();
            if (offered.size() >= 3) {
                targetRole = roleId;
                first = offered.get(0);
                second = offered.get(1);
                third = offered.get(2);
                break;
            }
        }

        assertNotNull(targetRole, "Test seed must offer one role to at least 3 players");

        assertEquals(RoleSelectionResult.SELECTED, service.select(first, targetRole));
        assertEquals(RoleSelectionResult.SELECTED, service.select(second, targetRole));
        assertEquals(RoleSelectionResult.ROLE_FULL, service.select(third, targetRole));
        assertEquals(2, service.selectedCount(targetRole));
    }

    @Test
    void cannotRegenerateAfterAnySelection() {
        RoleSelectionService service = new RoleSelectionService(
                DefaultRoleCatalog.createRegistry()
        );
        List<PlayerId> players = players(2);
        service.prepareCandidates(players, new Random(1));

        RoleId role = service.candidates(players.getFirst()).orElseThrow()
                .candidates().getFirst();
        service.select(players.getFirst(), role);

        assertThrows(
                IllegalStateException.class,
                () -> service.prepareCandidates(players, new Random(2))
        );
    }

    private static List<PlayerId> players(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(RoleSelectionServiceTest::player)
                .toList();
    }

    private static PlayerId player(int suffix) {
        return PlayerId.of(UUID.fromString(
                "00000000-0000-0000-0000-" + String.format("%012d", suffix)
        ));
    }
}

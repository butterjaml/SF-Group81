package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.CasualWorkApplicationCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkPostingCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import com.sfgroup81.tams.service.CasualWorkService;
import com.sfgroup81.tams.service.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CasualWorkBrowserPanelTest {
    @TempDir
    Path tempDir;

    @Test
    void nonHiredTaShouldStillSeeOpenTemporaryWorkPostings() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        User admin = userRepository.saveNewUser("Admin", "80001", "admin@example.com",
                SecurityUtil.sha256("password123"), UserRole.ADMIN);
        User ta = userRepository.saveNewUser("TA Una", "20250020", "una@example.com",
                SecurityUtil.sha256("password123"), UserRole.TA, TACategory.NON_MODULAR);

        CasualWorkService service = new CasualWorkService(
                new CasualWorkPostingCsvRepository(tempDir),
                new CasualWorkApplicationCsvRepository(tempDir),
                userRepository,
                new TAApplicationCsvRepository(tempDir),
                new PositionCsvRepository(tempDir)
        );
        service.createPosting(
                "Open Day Support",
                "Guide visitors",
                "2026-04-25",
                "Innovation Hub",
                "Communication",
                2,
                "180 yuan/session",
                admin.userId()
        );

        assertFalse(service.canApplyCasualWork(ta.userId()));

        AtomicReference<JTable> tableRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            CasualWorkBrowserPanel panel = new CasualWorkBrowserPanel(ta, service, () -> {
            });
            tableRef.set(findComponent(panel, JTable.class));
        });

        JTable table = tableRef.get();
        assertNotNull(table);
        assertEquals(1, table.getRowCount());
        assertEquals("CW0001", table.getValueAt(0, 0));
    }

    private static <T extends Component> T findComponent(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findComponent(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

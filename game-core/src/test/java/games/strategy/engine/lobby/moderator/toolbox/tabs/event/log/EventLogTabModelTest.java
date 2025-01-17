package games.strategy.engine.lobby.moderator.toolbox.tabs.event.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.event.log.ModeratorEvent;
import org.triplea.http.client.moderator.toolbox.event.log.ToolboxEventLogClient;

import games.strategy.engine.lobby.moderator.toolbox.tabs.ToolboxTabModelTestUtil;

@ExtendWith(MockitoExtension.class)
class EventLogTabModelTest {
  private static final ModeratorEvent EVENT_1 = ModeratorEvent.builder()
      .date(Instant.now())
      .actionTarget("Malaria is a cloudy pin.")
      .moderatorAction("Jolly roger, real pin. go to puerto rico.")
      .moderatorName("All parrots loot rainy, stormy fish.")
      .build();

  private static final ModeratorEvent EVENT_2 = ModeratorEvent.builder()
      .date(Instant.now().minusSeconds(1000L))
      .actionTarget("Strength is a gutless tuna.")
      .moderatorAction("Doubloons travel with booty at the stormy madagascar!")
      .moderatorName("The son crushes with life, love the lighthouse.")
      .build();

  private static final PagingParams PAGING_PARAMS = PagingParams.builder()
      .rowNumber(0)
      .pageSize(10)
      .build();


  @Mock
  private ToolboxEventLogClient toolboxEventLogClient;

  @InjectMocks
  private EventLogTabModel eventLogTabModel;

  /**
   * Simple test that fetches log table data and verifies the values. Here we mostly convert the conversion
   * from a list of beans to table data format, a list of lists.
   */
  @Test
  void getEventLogTableData() {
    when(toolboxEventLogClient.lookupModeratorEvents(PAGING_PARAMS))
        .thenReturn(Arrays.asList(EVENT_1, EVENT_2));

    final List<List<String>> tableData = eventLogTabModel.fetchTableData(PAGING_PARAMS);

    assertThat(tableData, hasSize(2));

    ToolboxTabModelTestUtil.verifyTableDimensions(tableData, EventLogTabModel.fetchTableHeaders());

    ToolboxTabModelTestUtil.verifyTableDataAtRow(tableData, 0,
        EVENT_1.getDate().toString(),
        EVENT_1.getModeratorName(),
        EVENT_1.getModeratorAction(),
        EVENT_1.getActionTarget());

    ToolboxTabModelTestUtil.verifyTableDataAtRow(tableData, 1,
        EVENT_2.getDate().toString(),
        EVENT_2.getModeratorName(),
        EVENT_2.getModeratorAction(),
        EVENT_2.getActionTarget());
  }
}

package io.mongock.driver.api.entry;

import io.mongock.api.exception.MongockException;
import io.mongock.driver.api.common.RepositoryIndexable;
import io.mongock.utils.Process;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.mongock.driver.api.entry.ChangeState.IGNORED;
import static io.mongock.driver.api.entry.ChangeState.FAILED;
import static io.mongock.driver.api.entry.ChangeState.ROLLBACK_FAILED;
import java.util.Date;


public interface ChangeEntryService extends RepositoryIndexable, Process {

  /**
   * NOT USED
   */
  @Deprecated
  default boolean isAlreadyExecuted(String changeSetId, String author) {
    throw new UnsupportedOperationException("THIS IS DEPRECATED AND WILL BE REMOVED");
  }

  /**
   * Retrieves a list with current executed entries ordered by execution timestamp.
   *
   * @return list of current executed entries ordered by execution timestamp
   * @throws MongockException
   */
  default List<ChangeEntryExecuted> getExecuted() throws MongockException {

    Predicate<ChangeEntry> cleanIrrelevantState = entry -> entry.getState() != IGNORED && entry.getState() != FAILED && entry.getState() != ROLLBACK_FAILED;
    return getEntriesMap()//Maps of List<ChangeEntry>, indexed by changeId
        .values()//collection of List<ChangeEntry>
        .stream()
        .map(duplicatedEntries -> duplicatedEntries.stream().filter(cleanIrrelevantState).collect(Collectors.toList()))//only takes into account executed or rolled back
        .filter(duplicatedEntries -> !duplicatedEntries.isEmpty())
        .map(duplicatedEntries -> duplicatedEntries.get(0))//transform each list in a single ChangeEntry(the first one)
        .sorted(Comparator.comparing(ChangeEntry::getOriginalTimestamp))// Sorts the resulting list chronologically
        .filter(ChangeEntry::isExecuted)//only gets the ones that are executed
        .map(ChangeEntryExecuted::new)//transform the entry to an executed entry
        .collect(Collectors.toList());
  }

  /**
   * Retrieves a list of the  entries in database with the current relevant state ordered by execution timestamp.
   *
   * @return list of the  entries in database with the current relevant state ordered by execution timestamp
   * @throws MongockException
   */
  default List<ChangeEntry> getAllEntriesWithCurrentState() throws MongockException {
    return getEntriesMap()//Maps of List<ChangeEntry>, indexed by changeId
        .values()//collection of List<ChangeEntry>
        .stream()
        .map(duplicatedEntries -> duplicatedEntries.stream().filter(ChangeEntry::hasRelevantState).collect(Collectors.toList()))//only takes into account relevant states
        .filter(duplicatedEntries -> !duplicatedEntries.isEmpty())
        .map(duplicatedEntries -> duplicatedEntries.get(0))//transform each list in a single ChangeEntry(the first one)
        .sorted(Comparator.comparing(ChangeEntry::getOriginalTimestamp))// Sorts the resulting list chronologically
        .collect(Collectors.toList());
  }

  default Map<String, List<ChangeEntry>> getEntriesMap() {
    Map<String, List<ChangeEntry>> log = getEntriesLog()
        .stream()
        .collect(Collectors.groupingBy(ChangeEntry::getChangeId));
    log.values().forEach(entries -> {
      entries.sort((c1, c2) -> c2.getTimestamp().compareTo(c1.getTimestamp()));//sorts each list in the map by date in reverse
      Date originalTimestamp = entries.get(entries.size()-1).getTimestamp();//gets original timestamp (oldest entry of group)
      entries.forEach(entry -> entry.setOriginalTimestamp(originalTimestamp));
    });
    return log;
  }

  /**
   * Returns all the changeEntries
   *
   * @return
   */
  List<ChangeEntry> getEntriesLog();


  /**
   * If there is already an ChangeEntry with the same  key(executionId, id and author), it's updated. Otherwise,
   * the new changeEntry is inserted.
   *
   * @param changeEntry Entry to be upsert
   * @throws MongockException if any i/o exception occurs
   */
  void saveOrUpdate(ChangeEntry changeEntry) throws MongockException;

  /**
   * Only for testing
   */
  void deleteAll();

}

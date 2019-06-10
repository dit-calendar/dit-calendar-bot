{-# LANGUAGE OverloadedStrings #-}

module Data.Repository.Acid.CalendarSpec (spec) where

import           Data.Acid                           (AcidState, query, update)
import           Data.Default                        (def)
import           Data.Maybe                          (fromJust, isJust,
                                                      isNothing)
import           Data.Time.Clock                     (UTCTime)
import           Test.Hspec

import           Data.Domain.CalendarEntry           as Calendar
import           Data.Domain.User                    as User
import           Data.Repository.Acid.DataBaseHelper (initDatabaseWithList)

import qualified Data.Repository.Acid.CalendarEntry  as CalendarEntryAcid
import qualified Data.Repository.Acid.InterfaceAcid  as InterfaceAcid

withDatabaseConnection :: (AcidState CalendarEntryAcid.EntryList -> IO ()) -> IO ()
withDatabaseConnection = initDatabaseWithList [initCalendar, initCalendar2, initCalendar3]

initUser :: User.User
initUser = def{User.userId = 1}

newDate = read "2012-11-19 17:51:42.203841 UTC"::UTCTime

initCalendar :: Calendar.CalendarEntry
initCalendar = def{ Calendar.entryId = 0, Calendar.userId = User.userId initUser, Calendar.description = "erster eintrag", Calendar.date = newDate}

initCalendar2 :: Calendar.CalendarEntry
initCalendar2 = def{ Calendar.entryId = 1, Calendar.userId = User.userId initUser, Calendar.description = "zweiter eintrag", Calendar.date = newDate}

initCalendar3 :: Calendar.CalendarEntry
initCalendar3 = def{ Calendar.entryId = 2, Calendar.userId = -1, Calendar.description = "sollte nicht angezeigt werden", Calendar.date = newDate}

spec :: Spec
spec =
    around withDatabaseConnection $
        context "Calendar" $
          describe "find" $
              it "all of user" $
                \c -> do
                  calendars   <- query c $ CalendarEntryAcid.AllEntriesForUser initUser
                  calendars `shouldBe` [initCalendar, initCalendar2]
module Presentation.Controller.CalendarController where

import           Data.Domain.CalendarEntry    as CalendarEntry
import           Data.Domain.Types            (EntryId, UserId)
import           Presentation.AcidHelper      (CtrlV)
import           Presentation.ResponseHelper  (okResponse, onEntryExist,
                                               onUserExist)

import qualified Data.Domain.User             as DomainUser
import qualified Data.Repository.CalendarRepo as CalendarRepo
import qualified Data.Service.CalendarEntry   as CalendarService


--handler for entryPage
entryPage :: EntryId -> CtrlV
entryPage i = onEntryExist i (\e -> okResponse $ "peeked at the description and saw: " ++ show e)

createCalendarEntry :: UserId -> String -> String -> DomainUser.User -> CtrlV
createCalendarEntry userId newDate description loggedUser = onUserExist userId createCalendar
    where createCalendar user = do
              entry <- CalendarService.createEntry newDate description user
              okResponse $ "Add Entry: " ++ show (CalendarEntry.entryId entry) ++ "to User: " ++ show userId

deleteCalendarEntry :: EntryId -> DomainUser.User -> CtrlV
deleteCalendarEntry i loggedUser = onEntryExist i deleteCalendar
    where deleteCalendar cEntry = do
              CalendarService.removeCalendar cEntry
              okResponse $ "CalendarEntry with id:" ++ show i ++ "deleted"

updateCalendarEntry :: EntryId -> String -> DomainUser.User -> CtrlV
updateCalendarEntry id description loggedUser = onEntryExist id updateCalendar
    where updateCalendar cEntry = do
              CalendarRepo.updateDescription cEntry description
              okResponse $ "CalendarEntry with id:" ++ show id ++ "updated"
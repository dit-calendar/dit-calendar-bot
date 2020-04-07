module Data.Task exposing (Model, Msg(..), Task, TaskMsg(..), emptyTask)

import Data.UIMessages exposing (Messages)
import Http
import Http.Detailed as HttpEx


type alias Task =
    { calendarEntryId : Maybe Int
    , taskId : Maybe Int
    , version : Int
    , description : String

    -- TODO , assignedUsers : List User
    , startDate : String
    , startTime : String
    , endDate : Maybe String
    , endTime : Maybe String
    }


emptyTask : Maybe Int -> Task
emptyTask calendarId =
    { calendarEntryId = calendarId, taskId = Nothing, version = 0, description = "", startDate = "", startTime = "", endDate = Nothing, endTime = Nothing }


type alias Model =
    { task : Task
    , messages : Messages
    }


type TaskMsg
    = Description String
      --TODO | assignedUsers (List User)
    | StartTime String
    | StartDate String
    | EndTime String
    | EndDate String


type Msg
    = TaskMsg TaskMsg
    | SaveTask
    | CreateTaskResult (Result (HttpEx.Error String) ( Http.Metadata, String ))
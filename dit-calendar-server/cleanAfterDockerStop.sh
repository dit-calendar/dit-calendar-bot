sudo find state/ -user root -exec sudo chown $USER: {} +
rm state/authenticate/core/open.lock state/authenticate/password/open.lock state/entryList/open.lock state/taskList/open.lock state/userList/open.lock
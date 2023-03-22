function saveUsers(no_of_users) {
  var shouldBreak = false;
  const startTimeInMicroseconds = performance.now() * 1000;
  for (i = 1; i <= no_of_users && !shouldBreak; i++) {
    $.ajax({
      type: "POST",
      url: "http://127.0.0.1:8081/bank/",
      data: { user: JSON.stringify(user(i)) },
      success: function (data) {
        if (data === "Failed to create user") {
          shouldBreak = true;
        }
      },
    });
    if (i == no_of_users) {
    finishTime(startTimeInMicroseconds,no_of_users);
    }
  }
}
function user(i) {
  username = "user" + i;
  //randomly choose initial balance of user and round it off to nearest hundred
  balance = Math.round((Math.random() * 1000000) / 100) * 100;
  //randomly choose a number btwn 1 - 5 that would determine the currency of a particular user
  currency_predictor = Math.floor(Math.random() * 5) + 1;
  switch (currency_predictor) {
    case 1:
      currency = "USD";
      break;
    case 2:
      currency = "KES";
      break;
    case 3:
      currency = "EUR";
      break;
    case 4:
      currency = "GBP";
      break;
    case 5:
      currency = "CAD";
      break;
    default:
      currency = "CHF";
  }
  password = "1234";
  //the created user
  return (username = {
    username: username,
    password: password,
    currency: currency,
    balance: balance,
  });
}

function finishTime(startTimeInMicroseconds,no_of_users){
  const finishTimeInMicroseconds = performance.now() * 1000;
  const timeDifferenceInMicroseconds = finishTimeInMicroseconds - startTimeInMicroseconds;
  const timeDifferenceInSeconds = Math.floor(timeDifferenceInMicroseconds / 1000000);
  const timeDifferenceInMilliseconds = Math.floor(timeDifferenceInMicroseconds / 1000) % 1000;
  const microseconds = timeDifferenceInMicroseconds % 1000;
  alert(`${no_of_users} users added in : ${timeDifferenceInSeconds} seconds, ${timeDifferenceInMilliseconds} milliseconds, ${microseconds} microseconds.`);
}

import { NativeBiometric } from '@capgo/capacitor-native-biometric';
import { SplashScreen } from '@capacitor/splash-screen';

// Simple function-based approach - no Web Components
function createBiometricTester() {
  // Create main container
  const container = document.createElement('div');
  container.style.cssText = `
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    padding: 20px;
    max-width: 800px;
    margin: 0 auto;
  `;

  // Create the HTML structure
  container.innerHTML = `
    <style>
      .section {
        margin-bottom: 30px;
        padding: 20px;
        border: 1px solid #ddd;
        border-radius: 8px;
        background: #f9f9f9;
      }
      .section h2 {
        margin-top: 0;
        color: #333;
      }
      .button {
        display: inline-block;
        padding: 10px 20px;
        margin: 5px;
        background-color: #007bff;
        color: #fff;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 14px;
      }
      .button:hover {
        background-color: #0056b3;
      }
      .button:disabled {
        background-color: #ccc;
        cursor: not-allowed;
      }
      .error {
        color: #dc3545;
        background: #f8d7da;
        padding: 10px;
        border-radius: 4px;
        margin: 10px 0;
      }
      .success {
        color: #155724;
        background: #d4edda;
        padding: 10px;
        border-radius: 4px;
        margin: 10px 0;
      }
      .info {
        color: #856404;
        background: #fff3cd;
        padding: 10px;
        border-radius: 4px;
        margin: 10px 0;
      }
      .form-group {
        margin-bottom: 15px;
      }
      .form-group label {
        display: block;
        margin-bottom: 5px;
        font-weight: bold;
      }
      .form-group input {
        width: 100%;
        padding: 8px;
        border: 1px solid #ddd;
        border-radius: 4px;
        box-sizing: border-box;
      }
      .credentials-display {
        background: #e9ecef;
        padding: 10px;
        border-radius: 4px;
        margin: 10px 0;
        font-family: monospace;
        max-height: 300px;
        overflow-y: auto;
      }
      .status {
        padding: 10px;
        margin: 10px 0;
        border-radius: 4px;
        font-weight: bold;
      }
      .status.available {
        background: #d4edda;
        color: #155724;
      }
      .status.unavailable {
        background: #f8d7da;
        color: #721c24;
      }
    </style>

    <h1>🔐 Native Biometric Test App</h1>

    <div class="section">
      <h2>Biometric Status</h2>
      <div id="biometric-status" class="status">Checking...</div>
      <button class="button" id="check-availability">Check Availability</button>
      <div id="biometric-info" class="info" style="display: none;"></div>
    </div>

    <div class="section">
      <h2>Authentication Test</h2>
      <button class="button" id="verify-identity">Verify Identity (Biometric)</button>
      <button class="button" id="verify-identity-fallback">Verify Identity (with Fallback)</button>
      <div id="auth-result"></div>
    </div>

    <div class="section">
      <h2>Credentials Management</h2>
      <div class="form-group">
        <label for="server">Server:</label>
        <input type="text" id="server" value="www.example.com" placeholder="Enter server URL">
      </div>
      <div class="form-group">
        <label for="username">Username:</label>
        <input type="text" id="username" value="testuser" placeholder="Enter username">
      </div>
      <div class="form-group">
        <label for="password">Password:</label>
        <input type="password" id="password" value="testpass123" placeholder="Enter password">
      </div>

      <button class="button" id="set-credentials">Set Credentials</button>
      <button class="button" id="get-credentials">Get Credentials</button>
      <button class="button" id="delete-credentials">Delete Credentials</button>
      <div id="credentials-result"></div>
    </div>

    <div class="section">
      <h2>Debug Console</h2>
      <div class="form-group">
        <label for="debug-input">Test Code:</label>
        <input type="text" id="debug-input" placeholder="Enter code to execute">
        <button class="button" id="execute-debug">Execute</button>
      </div>
      <div class="info">
        <strong>Quick Debug Commands:</strong><br>
        • Test the exact user issue: <code>NativeBiometric.setCredentials({username: 'test@example.com', password: 'password123', server: 'example.com'})</code><br>
        • Check availability: <code>NativeBiometric.isAvailable()</code><br>
        • Get credentials: <code>NativeBiometric.getCredentials({server: 'example.com'})</code>
      </div>
      <div id="debug-output" class="credentials-display"></div>
    </div>

    <div class="section">
      <h2>Console Logs</h2>
      <div id="console-logs" class="credentials-display"></div>
    </div>
  `;

  // Add event listeners
  const elements = {
    checkAvailability: container.querySelector('#check-availability'),
    verifyIdentity: container.querySelector('#verify-identity'),
    verifyIdentityFallback: container.querySelector('#verify-identity-fallback'),
    setCredentials: container.querySelector('#set-credentials'),
    getCredentials: container.querySelector('#get-credentials'),
    deleteCredentials: container.querySelector('#delete-credentials'),
    executeDebug: container.querySelector('#execute-debug'),
    debugInput: container.querySelector('#debug-input'),
    biometricStatus: container.querySelector('#biometric-status'),
    biometricInfo: container.querySelector('#biometric-info'),
    authResult: container.querySelector('#auth-result'),
    credentialsResult: container.querySelector('#credentials-result'),
    debugOutput: container.querySelector('#debug-output'),
    consoleLogs: container.querySelector('#console-logs'),
    server: container.querySelector('#server'),
    username: container.querySelector('#username'),
    password: container.querySelector('#password')
  };

  // Override console.log to capture logs
  const originalLog = console.log;
  const originalError = console.error;
  const originalWarn = console.warn;

  console.log = (...args) => {
    originalLog.apply(console, args);
    addConsoleLog('LOG', args);
  };

  console.error = (...args) => {
    originalError.apply(console, args);
    addConsoleLog('ERROR', args);
  };

  console.warn = (...args) => {
    originalWarn.apply(console, args);
    addConsoleLog('WARN', args);
  };

  function addConsoleLog(type, args) {
    const timestamp = new Date().toLocaleTimeString();
    const message = args.map(arg =>
      typeof arg === 'object' ? JSON.stringify(arg, null, 2) : String(arg)
    ).join(' ');

    elements.consoleLogs.innerHTML += `<div>[${timestamp}] ${type}: ${message}</div>`;
    elements.consoleLogs.scrollTop = elements.consoleLogs.scrollHeight;
  }

  // Event listeners
  elements.checkAvailability.addEventListener('click', checkAvailability);
  elements.verifyIdentity.addEventListener('click', () => verifyIdentity(false));
  elements.verifyIdentityFallback.addEventListener('click', () => verifyIdentity(true));
  elements.setCredentials.addEventListener('click', setCredentials);
  elements.getCredentials.addEventListener('click', getCredentials);
  elements.deleteCredentials.addEventListener('click', deleteCredentials);
  elements.executeDebug.addEventListener('click', executeDebug);

  // Functions
  async function checkAvailability() {
    try {
      addConsoleLog('INFO', ['Checking biometric availability...']);
      const result = await NativeBiometric.isAvailable();
      addConsoleLog('INFO', ['Availability result:', result]);

      if (result.isAvailable) {
        elements.biometricStatus.textContent = `✅ Biometrics Available (${getBiometryTypeName(result.biometryType)})`;
        elements.biometricStatus.className = 'status available';
        elements.biometricInfo.style.display = 'block';
        elements.biometricInfo.innerHTML = `<strong>Biometry Type:</strong> ${getBiometryTypeName(result.biometryType)} (${result.biometryType})`;
      } else {
        elements.biometricStatus.textContent = `❌ Biometrics Not Available (Error: ${result.errorCode || 'Unknown'})`;
        elements.biometricStatus.className = 'status unavailable';
        elements.biometricInfo.style.display = 'block';
        elements.biometricInfo.innerHTML = `<strong>Error Code:</strong> ${result.errorCode || 'Unknown'}`;
      }
    } catch (error) {
      addConsoleLog('ERROR', ['Availability check failed:', error]);
      elements.biometricStatus.textContent = '❌ Check Failed';
      elements.biometricStatus.className = 'status unavailable';
    }
  }

  async function verifyIdentity(useFallback) {
    try {
      addConsoleLog('INFO', [`Verifying identity (fallback: ${useFallback})...`]);
      await NativeBiometric.verifyIdentity({
        reason: 'Test biometric authentication',
        useFallback: useFallback
      });
      addConsoleLog('SUCCESS', ['✅ Identity verified successfully!']);
      showResult(elements.authResult, '✅ Identity verified successfully!', 'success');
    } catch (error) {
      addConsoleLog('ERROR', ['Identity verification failed:', error]);
      showResult(elements.authResult, `❌ Verification failed: ${error.message || error}`, 'error');
    }
  }

  async function setCredentials() {
    const server = elements.server.value;
    const username = elements.username.value;
    const password = elements.password.value;

    if (!server || !username || !password) {
      showResult(elements.credentialsResult, '❌ Please fill in all fields', 'error');
      return;
    }

    try {
      addConsoleLog('INFO', [`Setting credentials for server: ${server}, username: ${username}`]);
      await NativeBiometric.setCredentials({
        username: username,
        password: password,
        server: server,
      });
      addConsoleLog('SUCCESS', ['✅ Credentials saved successfully!']);
      showResult(elements.credentialsResult, '✅ Credentials saved successfully!', 'success');
    } catch (error) {
      addConsoleLog('ERROR', ['Set credentials failed:', error]);
      showResult(elements.credentialsResult, `❌ Save failed: ${error.message || error}`, 'error');
    }
  }

  async function getCredentials() {
    const server = elements.server.value;

    if (!server) {
      showResult(elements.credentialsResult, '❌ Please enter server', 'error');
      return;
    }

    try {
      addConsoleLog('INFO', [`Getting credentials for server: ${server}`]);
      const credentials = await NativeBiometric.getCredentials({
        server: server,
      });
      addConsoleLog('SUCCESS', ['✅ Credentials retrieved:', credentials]);
      showResult(elements.credentialsResult,
        `<strong>Retrieved Credentials:</strong><br>Username: ${credentials.username}<br>Password: ${credentials.password}`,
        'success');
    } catch (error) {
      addConsoleLog('ERROR', ['Get credentials failed:', error]);
      showResult(elements.credentialsResult, `❌ Get failed: ${error.message || error}`, 'error');
    }
  }

  async function deleteCredentials() {
    const server = elements.server.value;

    if (!server) {
      showResult(elements.credentialsResult, '❌ Please enter server', 'error');
      return;
    }

    try {
      addConsoleLog('INFO', [`Deleting credentials for server: ${server}`]);
      await NativeBiometric.deleteCredentials({
        server: server,
      });
      addConsoleLog('SUCCESS', ['✅ Credentials deleted successfully!']);
      showResult(elements.credentialsResult, '✅ Credentials deleted successfully!', 'success');
    } catch (error) {
      addConsoleLog('ERROR', ['Delete credentials failed:', error]);
      showResult(elements.credentialsResult, `❌ Delete failed: ${error.message || error}`, 'error');
    }
  }

  async function executeDebug() {
    const code = elements.debugInput.value.trim();

    if (!code) {
      showResult(elements.debugOutput, '❌ Please enter some code to execute', 'error');
      return;
    }

    try {
      addConsoleLog('INFO', [`Executing debug code: ${code}`]);

      // Simple eval for testing (be careful with this in production!)
      const result = await eval(`(async () => { ${code} })()`);

      addConsoleLog('SUCCESS', ['✅ Debug code executed successfully:', result]);
      showResult(elements.debugOutput, `✅ Result: ${JSON.stringify(result, null, 2)}`, 'success');
    } catch (error) {
      addConsoleLog('ERROR', ['Debug code execution failed:', error]);
      showResult(elements.debugOutput, `❌ Error: ${error.message || error}`, 'error');
    }
  }

  function showResult(element, message, type) {
    element.innerHTML = `<div class="${type}">${message}</div>`;
    element.style.display = 'block';

    // Auto-hide after 5 seconds for success messages
    if (type === 'success') {
      setTimeout(() => {
        element.style.display = 'none';
      }, 5000);
    }
  }

  function getBiometryTypeName(type) {
    const names = {
      0: 'None',
      1: 'Touch ID',
      2: 'Face ID',
      3: 'Fingerprint',
      4: 'Face Authentication',
      5: 'Iris Authentication',
      6: 'Multiple'
    };
    return names[type] || 'Unknown';
  }

  // Initialize
  console.log('🔍 Biometric Test App initialized. Ready to test!');

  // Hide splash screen immediately
  SplashScreen.hide().catch(err => {
    console.log('SplashScreen already hidden or not available:', err.message);
  });

  checkAvailability();

  return container;
}

// Create and append the tester
document.body.appendChild(createBiometricTester());
